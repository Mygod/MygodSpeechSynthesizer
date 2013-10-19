using System;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows;
using Microsoft.Windows.Controls.Ribbon;
using Mygod.SpeechSynthesiser.Dwm;

namespace Mygod.SpeechSynthesiser
{
    /// <summary>
    /// WPF Glass Window
    /// Inherit from this window class to enable glass on a WPF window
    /// </summary>
    public class GlassWindow : RibbonWindow
    {
        #region properties

        /// <summary>
        /// Get determines if AeroGlass is enabled on the desktop. Set enables/disables AreoGlass on the desktop.
        /// </summary>
        protected static bool AeroGlassCompositionEnabled
        {
            get
            {
                try
                {
                    return DesktopWindowManagerNativeMethods.DwmIsCompositionEnabled();
                }
                catch
                {
                    return false;
                }
            }
        }

        #endregion

        #region events

        /// <summary>
        /// Fires when the availability of Glass effect changes.
        /// </summary>
        public event EventHandler<AeroGlassCompositionChangedEventArgs> AeroGlassCompositionChanged;

        #endregion

        #region operations

        /// <summary>
        /// Makes the background of current window transparent from both Wpf and Windows Perspective
        /// </summary>
        protected void SetAeroGlassTransparency()
        {
            // Set the Background to transparent from Win32 perpective 
            var source = HwndSource.FromHwnd(windowHandle);
            if (source != null && source.CompositionTarget != null) source.CompositionTarget.BackgroundColor = Colors.Transparent;

            // Set the Background to transparent from WPF perpective 
            Background = Brushes.Transparent;
        }

        /// <summary>
        /// Resets the AeroGlass exclusion area.
        /// </summary>
        protected void ResetAeroGlass()
        {
            var margins = new Margins(true);
            try
            {
                DesktopWindowManagerNativeMethods.DwmExtendFrameIntoClientArea(windowHandle, ref margins);
            }
            catch (DllNotFoundException) { }
        }

        #endregion

        #region implementation
        private IntPtr windowHandle;

        private IntPtr WndProc(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
        {
            if (msg == DWMMessages.WmDWMCompositionChanged || msg == DWMMessages.WmDWMNcRenderingChanged)
            {
                if (AeroGlassCompositionChanged != null)
                {
                    AeroGlassCompositionChanged.Invoke(this,
                        new AeroGlassCompositionChangedEventArgs(AeroGlassCompositionEnabled));
                }

                handled = true;
            }
            return IntPtr.Zero;
        }

        /// <summary>
        /// OnSourceInitialized
        /// Override SourceInitialized to initialize windowHandle for this window.
        /// A valid windowHandle is available only after the sourceInitialized is completed
        /// </summary>
        /// <param name="e">EventArgs</param>
        protected override void OnSourceInitialized(EventArgs e)
        {
            base.OnSourceInitialized(e);
            var interopHelper = new WindowInteropHelper(this);
            windowHandle = interopHelper.Handle;

            // add Window Proc hook to capture DWM messages
            var source = HwndSource.FromHwnd(windowHandle);
            if (source != null) source.AddHook(WndProc);

            //ResetAeroGlass();
            SizeChanged += UpdateAero;
            AeroGlassCompositionChanged += UpdateAero;
            UpdateAero(AeroGlassCompositionEnabled);
        }

        #endregion

        #region Customs

        public bool AeroEnabled;
        private void UpdateAero(object sender = null, SizeChangedEventArgs e = null)
        {
            UpdateAero(AeroGlassCompositionEnabled);
        }
        private void UpdateAero(object sender, AeroGlassCompositionChangedEventArgs e)
        {
            UpdateAero(e.GlassAvailable);
        }
        private void UpdateAero(bool aeroGlassCompositionEnabled)
        {
            try
            {
                if (!aeroGlassCompositionEnabled)
                {
                    Background = Brushes.White;
                    AeroEnabled = false;
                }
                else
                {
                    ResetAeroGlass();
                    SetAeroGlassTransparency();
                    InvalidateVisual();
                    AeroEnabled = true;
                }
            }
            catch (DllNotFoundException)        // unsupport on XP & older devices
            {
                AeroEnabled = false;
            }
            catch (InvalidOperationException)   // window is too small
            {
                AeroEnabled = false;
            }
        }

        #endregion
    }
}
