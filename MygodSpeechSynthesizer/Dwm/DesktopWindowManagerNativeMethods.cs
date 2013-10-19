using System;
using System.Runtime.InteropServices;
using System.Security;

namespace Mygod.SpeechSynthesiser.Dwm
{
    internal static class DWMMessages
    {
        internal const int WmDWMCompositionChanged = 0x031E;
        internal const int WmDWMNcRenderingChanged = 0x031F;
    }

    [StructLayout(LayoutKind.Sequential)]
    internal struct Margins
    {
        private readonly int LeftWidth;      // width of left border that retains its size
        private readonly int RightWidth;     // width of right border that retains its size
        private readonly int TopHeight;      // height of top border that retains its size
        private readonly int BottomHeight;   // height of bottom border that retains its size

        public Margins(bool fullWindow)
        {
            LeftWidth = RightWidth = TopHeight = BottomHeight = (fullWindow ? -1 : 0);
        }
    };

    /// <summary>
    /// Internal class that contains interop declarations for 
    /// functions that are not benign and are performance critical. 
    /// </summary>
    [SuppressUnmanagedCodeSecurity]
    internal static class DesktopWindowManagerNativeMethods
    {
        [DllImport("DwmApi.dll")]
        internal static extern int DwmExtendFrameIntoClientArea(IntPtr hwnd, ref Margins m);

        [DllImport("DwmApi.dll", PreserveSig = false)]
        [return: MarshalAs(UnmanagedType.Bool)]
        internal static extern bool DwmIsCompositionEnabled();
    }
}
