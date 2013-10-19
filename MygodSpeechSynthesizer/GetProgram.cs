using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Media.Imaging;

namespace Mygod.SpeechSynthesiser
{
    static class GetProgram
    {
        private static AssemblyName NowAssemblyName { get { return Assembly.GetCallingAssembly().GetName(); } }
        public static string Name { get { return NowAssemblyName.Name; } }
        public static string Version { get { return NowAssemblyName.Version.ToString(); } }
        public static string Title { get { return NowAssemblyName.Name + " V" + NowAssemblyName.Version; } }
        public static string Path { get { return Application.ResourceAssembly.Location; } }
        private static BitmapSource mainIcon;
        public static BitmapSource MainIcon { get { return mainIcon ?? (mainIcon = IconExtractor.GetIcon(Path)); } }
        public static DateTime CompilationTime
        {
            get
            {
                var b = new byte[2048];
                using (var s = new FileStream(Assembly.GetCallingAssembly().Location, FileMode.Open, FileAccess.Read)) s.Read(b, 0, 2048);
                var dt = new DateTime(1970, 1, 1).AddSeconds(BitConverter.ToInt32(b, BitConverter.ToInt32(b, 60) + 8));
                return dt + TimeZone.CurrentTimeZone.GetUtcOffset(dt);
            }
        }
        public static string FileName { get { return Process.GetCurrentProcess().MainModule.FileName; } }
    }

    public class IconExtractor : IDisposable
    {
        #region Win32 interop.

        #region Unmanaged Types

        [UnmanagedFunctionPointer(CallingConvention.Winapi, CharSet = CharSet.Auto)]
        private delegate bool EnumResNameProc(IntPtr hModule, int lpszType, IntPtr lpszName, IconResInfo lParam);

        #endregion

        #region Consts.

        private const int LOAD_LIBRARY_AS_DATAFILE = 0x00000002;

        private const int RT_ICON = 3;
        private const int RT_GROUP_ICON = 14;

        private const int MAX_PATH = 260;

        private const int ERROR_FILE_NOT_FOUND = 2;
        private const int ERROR_BAD_EXE_FORMAT = 193;

        private const int sICONDIR = 6; // sizeof(ICONDIR) 
        private const int sICONDIRENTRY = 16; // sizeof(ICONDIRENTRY)
        private const int sGRPICONDIRENTRY = 14; // sizeof(GRPICONDIRENTRY)

        #endregion

        #region API Functions

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern IntPtr LoadLibrary(string lpFileName);

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern IntPtr LoadLibraryEx(string lpFileName, IntPtr hFile, int dwFlags);

        [DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
        private static extern bool FreeLibrary(IntPtr hModule);

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern int GetModuleFileName(IntPtr hModule, StringBuilder lpFilename, int nSize);

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern bool EnumResourceNames(
            IntPtr hModule, int lpszType, EnumResNameProc lpEnumFunc, IconResInfo lParam);

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern IntPtr FindResource(IntPtr hModule, IntPtr lpName, int lpType);

        [DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
        private static extern IntPtr LoadResource(IntPtr hModule, IntPtr hResInfo);

        [DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
        private static extern IntPtr LockResource(IntPtr hResData);

        [DllImport("kernel32.dll", SetLastError = true, ExactSpelling = true)]
        private static extern int SizeofResource(IntPtr hModule, IntPtr hResInfo);

        #endregion

        #endregion

        #region Managed Types

        private class IconResInfo
        {
            public readonly List<ResourceName> IconNames = new List<ResourceName>();
        }

        private class ResourceName
        {
            public IntPtr Id { get; private set; }
            public string Name { get; private set; }

            private IntPtr _bufPtr = IntPtr.Zero;

            public ResourceName(IntPtr lpName)
            {
                if (((uint)lpName >> 16) == 0) // #define IS_INTRESOURCE(_r) ((((ULONG_PTR)(_r)) >> 16) == 0)
                {
                    Id = lpName;
                    Name = null;
                }
                else
                {
                    Id = IntPtr.Zero;
                    Name = Marshal.PtrToStringAuto(lpName);
                }
            }

            public IntPtr GetValue()
            {
                if (Name == null)
                {
                    return Id;
                }
                else
                {
                    _bufPtr = Marshal.StringToHGlobalAuto(Name);
                    return _bufPtr;
                }
            }

            public void Free()
            {
                if (_bufPtr != IntPtr.Zero)
                {
                    try
                    {
                        Marshal.FreeHGlobal(_bufPtr);
                    }
                    catch
                    {
                    }

                    _bufPtr = IntPtr.Zero;
                }
            }
        }

        #endregion

        #region Private Fields

        private IntPtr _hModule = IntPtr.Zero;
        private readonly IconResInfo _resInfo;

        private Icon[] _iconCache;

        #endregion

        #region Public Properties

        private readonly string _filename;

        // Full path 
        public string Filename
        {
            get { return _filename; }
        }

        public int IconCount
        {
            get { return _resInfo.IconNames.Count; }
        }

        #endregion

        #region Contructor/Destructor and relatives

        /// <summary>
        /// Load the specified executable file or DLL, and get ready to extract the icons.
        /// </summary>
        /// <param name="filename">The name of a file from which icons will be extracted.</param>
        public IconExtractor(string filename)
        {
            if (filename == null)
            {
                throw new ArgumentNullException("filename");
            }

            _hModule = LoadLibrary(filename);
            if (_hModule == IntPtr.Zero)
            {
                _hModule = LoadLibraryEx(filename, IntPtr.Zero, LOAD_LIBRARY_AS_DATAFILE);
                if (_hModule == IntPtr.Zero)
                {
                    switch (Marshal.GetLastWin32Error())
                    {
                        case ERROR_FILE_NOT_FOUND:
                            throw new FileNotFoundException("Specified file '" + filename + "' not found.");

                        case ERROR_BAD_EXE_FORMAT:
                            throw new ArgumentException("Specified file '" + filename +
                                                        "' is not an executable file or DLL.");

                        default:
                            throw new Win32Exception();
                    }
                }
            }

            var buf = new StringBuilder(MAX_PATH);
            GetModuleFileName(_hModule, buf, buf.Capacity + 1);
            _filename = filename;

            _resInfo = new IconResInfo();
            bool success = EnumResourceNames(_hModule, RT_GROUP_ICON, EnumResNameCallBack, _resInfo);
            if (!success)
            {
                throw new Win32Exception();
            }

            _iconCache = new Icon[IconCount];
        }

        ~IconExtractor()
        {
            Dispose();
        }

        public void Dispose()
        {
            if (_hModule != IntPtr.Zero)
            {
                try
                {
                    FreeLibrary(_hModule);
                }
                catch
                {
                }

                _hModule = IntPtr.Zero;
            }

            if (_iconCache != null)
            {
                foreach (Icon i in _iconCache)
                {
                    if (i != null)
                    {
                        try
                        {
                            i.Dispose();
                        }
                        catch
                        {
                        }
                    }
                }

                _iconCache = null;
            }
        }

        #endregion

        #region Public Methods

        /// <summary>
        /// Extract an icon from the loaded executable file or DLL. 
        /// </summary>
        /// <param name="iconIndex">The zero-based index of the icon to be extracted.</param>
        /// <returns>A System.Drawing.Icon object which may consists of multiple icons.</returns>
        /// <remarks>Always returns new copy of the Icon. It should be disposed by the user.</remarks>
        public Icon GetIcon(int iconIndex)
        {
            if (_hModule == IntPtr.Zero)
            {
                throw new ObjectDisposedException("IconExtractor");
            }

            if (iconIndex < 0 || IconCount <= iconIndex)
            {
                throw new ArgumentException(
                    "iconIndex is out of range. It should be between 0 and " + (IconCount - 1).ToString() + ".");
            }

            if (_iconCache[iconIndex] == null)
            {
                _iconCache[iconIndex] = CreateIcon(iconIndex);
            }

            return (Icon)_iconCache[iconIndex].Clone();
        }

        /// <summary>
        /// Split an Icon consists of multiple icons into an array of Icon each consist of single icons.
        /// </summary>
        /// <param name="icon">The System.Drawing.Icon to be split.</param>
        /// <returns>An array of System.Drawing.Icon each consist of single icons.</returns>
        public static IEnumerable<Icon> SplitIcon(Icon icon)
        {
            if (icon == null)
            {
                throw new ArgumentNullException("icon");
            }

            // Get multiple .ico file image.
            byte[] srcBuf;
            using (var stream = new MemoryStream())
            {
                icon.Save(stream);
                srcBuf = stream.ToArray();
            }

            var splitIcons = new List<Icon>();
            {
                int count = BitConverter.ToInt16(srcBuf, 4); // ICONDIR.idCount

                for (int i = 0; i < count; i++)
                {
                    using (var destStream = new MemoryStream())
                    using (var writer = new BinaryWriter(destStream))
                    {
                        // Copy ICONDIR and ICONDIRENTRY.
                        writer.Write(srcBuf, 0, sICONDIR - 2);
                        writer.Write((short)1); // ICONDIR.idCount == 1;

                        writer.Write(srcBuf, sICONDIR + sICONDIRENTRY * i, sICONDIRENTRY - 4);
                        writer.Write(sICONDIR + sICONDIRENTRY);
                        // ICONDIRENTRY.dwImageOffset = sizeof(ICONDIR) + sizeof(ICONDIRENTRY)

                        // Copy picture and mask data.
                        int imgSize = BitConverter.ToInt32(srcBuf, sICONDIR + sICONDIRENTRY * i + 8);
                        // ICONDIRENTRY.dwBytesInRes
                        int imgOffset = BitConverter.ToInt32(srcBuf, sICONDIR + sICONDIRENTRY * i + 12);
                        // ICONDIRENTRY.dwImageOffset
                        writer.Write(srcBuf, imgOffset, imgSize);

                        // Create new icon.
                        destStream.Seek(0, SeekOrigin.Begin);
                        try
                        {
                            splitIcons.Add(new Icon(destStream));
                        }
                        catch (Win32Exception)  // creating 256x256 icon on XP will fail
                        {
                        }
                    }
                }
            }

            return splitIcons;
        }

        public static BitmapSource GetIcon(string executionPath, int index = 0)
        {
            var icon = SplitIcon(new IconExtractor(executionPath).GetIcon(index)).OrderByDescending(i => i.Height).First();
            return icon == null ? null
                       : Imaging.CreateBitmapSourceFromHIcon(icon.Handle, Int32Rect.Empty, BitmapSizeOptions.FromEmptyOptions());
        }

        public override string ToString()
        {
            string text = String.Format("IconExtractor (Filename: '{0}', IconCount: {1})", Filename, IconCount);
            return text;
        }

        #endregion

        #region Private Methods

        private bool EnumResNameCallBack(IntPtr hModule, int lpszType, IntPtr lpszName, IconResInfo lParam)
        {
            // Callback function for EnumResourceNames().

            if (lpszType == RT_GROUP_ICON)
            {
                lParam.IconNames.Add(new ResourceName(lpszName));
            }

            return true;
        }

        private Icon CreateIcon(int iconIndex)
        {
            // Get group icon resource.
            byte[] srcBuf = GetResourceData(_hModule, _resInfo.IconNames[iconIndex], RT_GROUP_ICON);

            // Convert the resouce into an .ico file image.
            using (var destStream = new MemoryStream())
            using (var writer = new BinaryWriter(destStream))
            {
                int count = BitConverter.ToUInt16(srcBuf, 4); // ICONDIR.idCount
                int imgOffset = sICONDIR + sICONDIRENTRY * count;

                // Copy ICONDIR.
                writer.Write(srcBuf, 0, sICONDIR);

                for (int i = 0; i < count; i++)
                {
                    // Copy GRPICONDIRENTRY converting into ICONDIRENTRY.
                    writer.BaseStream.Seek(sICONDIR + sICONDIRENTRY * i, SeekOrigin.Begin);
                    writer.Write(srcBuf, sICONDIR + sGRPICONDIRENTRY * i, sICONDIRENTRY - 4);
                    // Common fields of structures
                    writer.Write(imgOffset); // ICONDIRENTRY.dwImageOffset

                    // Get picture and mask data, then copy them.
                    var nID = (IntPtr)BitConverter.ToUInt16(srcBuf, sICONDIR + sGRPICONDIRENTRY * i + 12);
                    // GRPICONDIRENTRY.nID
                    byte[] imgBuf = GetResourceData(_hModule, nID, RT_ICON);

                    writer.BaseStream.Seek(imgOffset, SeekOrigin.Begin);
                    writer.Write(imgBuf, 0, imgBuf.Length);

                    imgOffset += imgBuf.Length;
                }

                destStream.Seek(0, SeekOrigin.Begin);
                return new Icon(destStream);
            }
        }

        private byte[] GetResourceData(IntPtr hModule, IntPtr lpName, int lpType)
        {
            // Get binary image of the specified resource.

            IntPtr hResInfo = FindResource(hModule, lpName, lpType);
            if (hResInfo == IntPtr.Zero)
            {
                throw new Win32Exception();
            }

            IntPtr hResData = LoadResource(hModule, hResInfo);
            if (hResData == IntPtr.Zero)
            {
                throw new Win32Exception();
            }

            IntPtr hGlobal = LockResource(hResData);
            if (hGlobal == IntPtr.Zero)
            {
                throw new Win32Exception();
            }

            int resSize = SizeofResource(hModule, hResInfo);
            if (resSize == 0)
            {
                throw new Win32Exception();
            }

            var buf = new byte[resSize];
            Marshal.Copy(hGlobal, buf, 0, buf.Length);

            return buf;
        }

        private byte[] GetResourceData(IntPtr hModule, ResourceName name, int lpType)
        {
            try
            {
                IntPtr lpName = name.GetValue();
                return GetResourceData(hModule, lpName, lpType);
            }
            finally
            {
                name.Free();
            }
        }

        #endregion
    }
}
