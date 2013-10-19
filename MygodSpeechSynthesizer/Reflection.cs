using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;

namespace Mygod.SpeechSynthesiser
{
    /// <summary>
    /// 给指定元素加上投影。（长度=子元素.Margin.Bottom）
    /// </summary>
    public class Reflection : Decorator
    {
        protected override void OnRender(DrawingContext drawingContext)
        {
            base.OnRender(drawingContext);
            var ctrl = Child as Control;
            if (ctrl == null) throw new NotSupportedException();
            drawingContext.PushTransform(new ScaleTransform(1, -1));
            drawingContext.PushOpacityMask(new LinearGradientBrush(new GradientStopCollection 
                {new GradientStop(Colors.Transparent, 0), new GradientStop(Color.FromArgb(0x88, 0, 0, 0), 1)}, 90));
            drawingContext.DrawRectangle(new VisualBrush(Child), null, new Rect(0, -ctrl.ActualHeight - ctrl.Margin.Bottom, 
                                                                                ctrl.ActualWidth, ctrl.Margin.Bottom));
            drawingContext.Pop();
            drawingContext.Pop();
        }
    }
}
