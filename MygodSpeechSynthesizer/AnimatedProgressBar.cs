namespace Mygod.SpeechSynthesiser
{
    using System;
    using System.ComponentModel;
    using System.Windows;
    using System.Windows.Controls;
    using System.Windows.Media.Animation;

    /// <summary>
    /// 指示操作进度。当改变时有动画。
    /// </summary>
    [TemplatePart(Name = "PART_GlowRect", Type = typeof(FrameworkElement)), TemplatePart(Name = "PART_Indicator", Type = typeof(FrameworkElement)),
     TemplatePart(Name = "PART_Track", Type = typeof(FrameworkElement))]
    public class AnimatedProgressBar : ProgressBar
    {
        /// <summary>
        /// 构造一个 Mygod.Windows.Controls.AnimatedProgressBar。
        /// </summary>
        public AnimatedProgressBar()
        {
            DependencyPropertyDescriptor.FromProperty(IsIndeterminateProperty, typeof(ProgressBar)).AddValueChanged(this, IndeterminateChanged);
            SizeChanged += OnSizeChanged;
        }

        private static void IndeterminateChanged(object sender, EventArgs e)
        {
            var bar = ((AnimatedProgressBar)sender);
            if (bar.IsIndeterminate) return;
            bar.indicator.BeginAnimation(WidthProperty, null);
            bar.indicator.Width = bar.track.ActualWidth * bar.Percent;
        }

        private FrameworkElement indicator, glow, track;

        /// <summary>
        /// 在将模板应用于 Mygod.Windows.Controls.AnimatedProgressBar 时调用。
        /// </summary>
        public override void OnApplyTemplate()
        {
            base.OnApplyTemplate();
            if (indicator != null) indicator.SizeChanged -= UpdateAnimation;
            indicator = GetTemplateChild("PART_Indicator") as FrameworkElement;
            glow = GetTemplateChild("PART_GlowRect") as FrameworkElement;
            track = GetTemplateChild("PART_Track") as FrameworkElement;
            if (indicator != null) indicator.SizeChanged += UpdateAnimation;
        }

        private double Percent { get { return (IsIndeterminate || Maximum <= Minimum) ? 1.0 : ((Value - Minimum) / (Maximum - Minimum)); } }

        private void StartAnimation()
        {
            if (indicator != null) indicator.BeginAnimation(WidthProperty, new DoubleAnimation
            {
                To = track.ActualWidth * Percent,
                Duration = new Duration(TimeSpan.FromMilliseconds(500)),
                EasingFunction = new PowerEase { EasingMode = EasingMode.EaseOut, Power = 2 }
            });
        }

        private void UpdateAnimation(object sender, SizeChangedEventArgs e)
        {
            if (glow == null) return;
            if ((IsVisible && (glow.Width > 0.0)) && (indicator.Width > 0.0))
            {
                double left = indicator.Width + glow.Width, num2 = -1.0 * glow.Width;
                TimeSpan keyTime = TimeSpan.FromSeconds((int)(left - num2) / 200.0), span2 = TimeSpan.FromSeconds(1.0), zero;
                if (glow.Margin.Left > num2 && glow.Margin.Left < left - 1.0) zero = TimeSpan.FromSeconds((-1.0 * (glow.Margin.Left - num2)) / 200.0);
                else zero = TimeSpan.Zero;
                var animation = new ThicknessAnimationUsingKeyFrames { BeginTime = zero, Duration = new Duration(keyTime + span2), RepeatBehavior = RepeatBehavior.Forever };
                animation.KeyFrames.Add(new LinearThicknessKeyFrame(new Thickness(num2, 0.0, 0.0, 0.0), TimeSpan.FromSeconds(0.0)));
                animation.KeyFrames.Add(new LinearThicknessKeyFrame(new Thickness(left, 0.0, 0.0, 0.0), keyTime));
                glow.BeginAnimation(MarginProperty, animation);
            }
            else
            {
                glow.BeginAnimation(MarginProperty, null);
            }
        }

        protected override void OnMaximumChanged(double oldMaximum, double newMaximum)
        {
            StartAnimation();
        }

        protected override void OnMinimumChanged(double oldMinimum, double newMinimum)
        {
            StartAnimation();
        }

        protected override void OnValueChanged(double oldValue, double newValue)
        {
            var e = new RoutedPropertyChangedEventArgs<double>(oldValue, newValue) { RoutedEvent = ValueChangedEvent };
            RaiseEvent(e);
            StartAnimation();
        }

        private void OnSizeChanged(object sender, SizeChangedEventArgs e)
        {
            StartAnimation();
        }
    }
}