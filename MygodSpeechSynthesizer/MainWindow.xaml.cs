using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Speech.AudioFormat;
using System.Speech.Recognition;
using System.Speech.Synthesis;
using System.Text;
using System.Threading;
using System.Windows;
using System.Windows.Data;
using System.Windows.Input;
using System.Xml;
using Microsoft.Win32;
using Microsoft.Windows.Controls.Ribbon;
using Mygod.SpeechSynthesiser.Dwm;
using TaskbarItemProgressState = System.Windows.Shell.TaskbarItemProgressState;

namespace Mygod.SpeechSynthesiser
{
    public partial class MainWindow
    {
        public MainWindow()
        {
            InitializeComponent();
            var infos = synthesizer.GetInstalledVoices().Select(voice => voice.VoiceInfo).ToList();
            VoiceBox.ItemsSource = infos;
            SelectedVoice = infos.FirstOrDefault(info => info.Culture.Name == Thread.CurrentThread.CurrentCulture.Name)
                ?? infos.FirstOrDefault();
            synthesizer.SetOutputToNull();
            synthesizer.SpeakProgress += UpdateProgress;
            synthesizer.SpeakCompleted += Completed;
            EnterTextBox.Text = string.Format(Properties.Resources.WelcomingText, GetProgram.Version,
                                              GetProgram.CompilationTime.ToShortDateString(), 
                                              GetProgram.CompilationTime.ToLongTimeString());
        }

        private void Glow(object sender, AeroGlassCompositionChangedEventArgs e)
        {
            if (e.GlassAvailable) Resources.Add(typeof(Ribbon), Resources["GlowingEffect"]);
            else Resources.Remove(typeof(Ribbon));
        }

        private void Drag(object sender, MouseButtonEventArgs e)
        {
            DragMove();
            if (e.ClickCount == 2) WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        }

        private void SpeechRecognized(object sender, SpeechRecognizedEventArgs e)
        {
            EnterTextBox.SelectedText = e.Result.Text;
            EnterTextBox.CaretIndex += EnterTextBox.SelectedText.Length;
            EnterTextBox.SelectionLength = 0;
        }

        private readonly SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        private SpeechRecognizer recognizer;

        private readonly OpenFileDialog openDialog = new OpenFileDialog
            { Title = "请选择要阅读的文件：", Filter = "文本文件 (*.txt)|*.txt|SSML 文件 (*.ssml)|*.ssml" };
        private readonly SaveFileDialog saveDialog = new SaveFileDialog
            { Title = "请选择要保存的路径：", Filter = "文本文件 (*.txt)|*.txt|SSML 文件 (*.ssml)|*.ssml" };
        private readonly OpenFileDialog openAudioDialog = new OpenFileDialog
            { Title = "请选择要插入的路径：", Filter = "所有文件 (*.*)|*" };
        private readonly SaveFileDialog saveWavDialog = new SaveFileDialog
            { Title = "请选择要输出的文件：", Filter = "波形文件 (*.wav; *.raw)|*.wav; *.raw" };

        private bool isWorking;
        private bool IsWorking
        {
            get { return isWorking; }
            set
            {
                EnterTextBox.IsReadOnly = value;
#pragma warning disable 665
                if (!(isWorking = value)) return;
#pragma warning restore 665
                Progress.IsIndeterminate = false;
                Taskbar.ProgressState = TaskbarItemProgressState.Normal;
            }
        }

        private StreamWriter lrcWriter;
        private int lastPosition, lastLength;

        private VoiceInfo SelectedVoice
        {
            get { return VoiceBox.SelectedItem as VoiceInfo; }
            set { VoiceBox.SelectedItem = value; }
        }

        private void UpdateProgress(object sender, SpeakProgressEventArgs e)
        {
            Progress.Value = (double)e.CharacterPosition / EnterTextBox.Text.Length;
            if (lrcWriter != null && (lastPosition != e.CharacterPosition || lastLength != e.CharacterCount))
            {
                var minutes = Math.Floor(e.AudioPosition.TotalMinutes);
                var seconds = e.AudioPosition.TotalSeconds - minutes * 60;
                var start = lastPosition + lastLength;
                var text = EnterTextBox.Text.Substring(start, e.CharacterCount + e.CharacterPosition - start);
                var i = text.IndexOf(Environment.NewLine, StringComparison.Ordinal);
                var newLine = false;
                while (i >= 0)
                {
                    newLine = true;
                    lrcWriter.Write("{2}{3}[{0}:{1:0.00}]", minutes, seconds, text.Substring(0, i), Environment.NewLine);
                    text = text.Remove(0, i + 2);
                    i = text.IndexOf(Environment.NewLine, StringComparison.Ordinal);
                }
                if (!newLine) lrcWriter.Write("{2}{0}:{1:0.00}{3}", minutes, seconds,
                                              LrcEnhancedBox.IsEnabled && LrcEnhancedBox.IsChecked == true ? "<" : "\r\n[",
                                              LrcEnhancedBox.IsEnabled && LrcEnhancedBox.IsChecked == true ? '>' : ']');
                lrcWriter.Write(text);

            }
            lastPosition = e.CharacterPosition;
            lastLength = e.CharacterCount;
            EnterTextBox.Select(lastPosition, e.CharacterCount);
            EnterTextBox.ScrollToLine(EnterTextBox.GetLineIndexFromCharacterIndex(e.CharacterPosition));
            EnterTextBox.Focus();
        }

        private void Completed(object sender, SpeakCompletedEventArgs e)
        {
            IsWorking = false;
            SynthesizeButton.Visibility = Visibility.Visible;
            StopButton.Visibility = Visibility.Collapsed;
            Progress.Value = 1;
            synthesizer.SetOutputToNull();
            if (!e.Cancelled && e.Error != null)
                MessageBox.Show("出现错误，详细信息：" + e.Error.Message, "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            if (lrcWriter == null) return;
            var start = lastPosition + lastLength;
            lrcWriter.Write(EnterTextBox.Text.Substring(start, EnterTextBox.Text.Length - start));
            lrcWriter.Close();
            lrcWriter = null;
        }

        private static string GetFileName(string path, string extension)
        {
            if (!path.ToLower().EndsWith(".wav")) return path + extension;
            return path.Substring(0, path.Length - 4) + extension;
        }

        private bool GeneratePrompt()
        {
            try
            {
                if (IsWorking)
                {
                    synthesizer.SpeakAsyncCancelAll();
                    IsWorking = false;
                    if (lrcWriter != null)
                    {
                        lrcWriter.Close();
                        lrcWriter = null;
                    }
                    return true;
                }
                lastPosition = lastLength = 0;
                IsWorking = true;
                Progress.Value = 0;
                synthesizer.Rate = (int)Math.Round(RateSlider.Value);
                synthesizer.Volume = (int)Math.Round(VolumeSlider.Value);
                synthesizer.SelectVoice(SelectedVoice.Name);
                return false;
            }
            catch (Exception exc)
            {
                MessageBox.Show("出现错误，详细信息：" + exc.Message, "错误", MessageBoxButton.OK, MessageBoxImage.Error);
                return true;
            }
        }

        private void Accept(object sender, RoutedEventArgs e)
        {
            if (GeneratePrompt())
            {
                SynthesizeButton.Visibility = Visibility.Visible;
                StopButton.Visibility = Visibility.Collapsed;
                return;
            }
            SynthesizeButton.Visibility = Visibility.Collapsed;
            StopButton.Visibility = Visibility.Visible;
            try
            {
                if (OutputFileBox.IsChecked != true) synthesizer.SetOutputToDefaultAudioDevice();
                else
                {
                    if (AudioFormatBox.SelectedItem == null) synthesizer.SetOutputToWaveFile(PositionBox.Text);
                    else synthesizer.SetOutputToWaveFile(PositionBox.Text, (SpeechAudioFormatInfo)AudioFormatBox.SelectedItem);
                    if (LrcBox.IsEnabled && LrcBox.IsChecked == true)
                    {
                        lrcWriter = new StreamWriter(GetFileName(PositionBox.Text, ".lrc"), false,
                                                     UTF8EnabledBox.IsEnabled && UTF8EnabledBox.IsChecked == true ? Encoding.UTF8
                                                                                                                  : Encoding.GetEncoding("GB2312"));
                        lrcWriter.Write("[0:0.00]");
                    }
                }
                if (SsmlBox.IsChecked == true) synthesizer.SpeakSsmlAsync(EnterTextBox.Text);
                else synthesizer.SpeakAsync(EnterTextBox.Text);
            }
            catch (Exception exc)
            {
                MessageBox.Show("出现错误，详细信息：" + exc.Message, "错误", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        private void Browse(object sender, RoutedEventArgs e)
        {
            if (saveWavDialog.ShowDialog() == true) PositionBox.Text = saveWavDialog.FileName;
        }

        private void BrowseFile(object sender, RoutedEventArgs e)
        {
            if (openDialog.ShowDialog() != true) return;
            EnterTextBox.Text = File.ReadAllText(openDialog.FileName);
            if (openDialog.FileName.ToLower().EndsWith(".ssml")) SsmlBox.IsChecked = true;
        }
        private void SaveToFile(object sender, ExecutedRoutedEventArgs e)
        {
            saveDialog.FilterIndex = SsmlBox.IsChecked == true ? 1 : 0;
            if (saveDialog.ShowDialog() == true) File.WriteAllText(saveDialog.FileName, EnterTextBox.Text);
        }

        private void DisplaySsmlHelp(object sender, RoutedEventArgs e)
        {
            Process.Start("http://www.w3.org/TR/speech-synthesis");
        }

        private void EnableSpeechRecognitionChanged(object sender, ExecutedRoutedEventArgs e)
        {
            if (EnableSpeechRecognition.IsChecked == true && recognizer == null)
            {
                recognizer = new SpeechRecognizer();
                recognizer.SpeechRecognized += SpeechRecognized;
            }
            if (recognizer != null && EnableSpeechRecognition.IsChecked != null)
                recognizer.Enabled = EnableSpeechRecognition.IsChecked.Value;
        }

        private void LrcBoxChanged()
        {
            var exp = LrcEnhancedBox.GetBindingExpression(IsEnabledProperty);
            if (exp != null) exp.UpdateTarget();
            exp = UTF8EnabledBox.GetBindingExpression(IsEnabledProperty);
            if (exp != null) exp.UpdateTarget();
        }
        private void LrcBoxChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            LrcBoxChanged();
        }
        private void LrcBoxChanged(object sender, RoutedEventArgs e)
        {
            LrcBoxChanged();
        }

        private static string GetString(XmlDocument doc)
        {
            using (var ms = new MemoryStream(20000))
            {
                using (var xw = XmlWriter.Create(ms, new XmlWriterSettings { Indent = true })) doc.Save(xw);
                ms.Position = 0;    //reset to 0
                using (var sr = new StreamReader(ms)) return sr.ReadToEnd();
            }
        }

        private void ConvertSsml(object sender, ExecutedRoutedEventArgs e)
        {
            if (GeneratePrompt()) return;
            var readingPrompt = new PromptBuilder();
            if (SsmlBox.IsChecked == true) readingPrompt.AppendSsml(new XmlTextReader(new MemoryStream(Encoding.UTF8.GetBytes(EnterTextBox.Text))));
            else readingPrompt.AppendText(EnterTextBox.Text);
            SsmlBox.IsChecked = true;
            var doc = new XmlDocument();
            doc.LoadXml(readingPrompt.ToXml());
            EnterTextBox.Text = GetString(doc);
            IsWorking = false;
        }

        private void GiveDefaultFormat(object sender, RoutedEventArgs e)
        {
            EnterTextBox.Text = Properties.Resources.DefaultSSML;
            var start = EnterTextBox.Text.IndexOf("<s>", StringComparison.Ordinal) + 3;
            EnterTextBox.Select(start, EnterTextBox.Text.IndexOf("</s>", StringComparison.Ordinal) - start);
            EnterTextBox.Focus();
        }

        private void InsertBetweenSelection(string left, string right, int selectLeft = -1, int selectLength = -1)
        {
            int start = EnterTextBox.SelectionStart, end = start + EnterTextBox.SelectionLength;
            EnterTextBox.Text = EnterTextBox.Text.Insert(end, right).Insert(start, left);
            EnterTextBox.Select(start + (selectLeft < 0 ? left.Length : selectLeft), (selectLength < 0 ? end - start : selectLength));
            EnterTextBox.Focus();
        }
        private void InsertXmlElement(string name, string attributes = "", bool selectFirstAttribute = false)
        {
            if (selectFirstAttribute)
            {
                var i = attributes.IndexOf('"');
                InsertBetweenSelection('<' + name + attributes + '>', "</" + name + '>', name.Length + i + 2, attributes.Substring(i + 1).IndexOf('"'));
            }
            else InsertBetweenSelection('<' + name + attributes + '>', "</" + name + '>');
        }
        private void ReplaceSelection(string text, int selectLeft = -1, int selectLength = 0)
        {
            EnterTextBox.SelectedText = text;
            EnterTextBox.Select(EnterTextBox.SelectionStart + (selectLeft < 0 ? EnterTextBox.SelectionLength : selectLeft), selectLength);
        }
        private void InsertSelfCloseXmlElement(string name, string attributes = "", bool selectFirstAttribute = false)
        {
            if (selectFirstAttribute)
            {
                var i = attributes.IndexOf('"');
                ReplaceSelection('<' + name + attributes + " />", name.Length + i + 2, attributes.Substring(i + 1).IndexOf('"'));
            }
            else ReplaceSelection('<' + name + attributes + " />");
        }

        private void InsertComment(object sender, RoutedEventArgs e)
        {
            InsertBetweenSelection("<-- ", " -->");
        }

        private void InsertParagraph(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("p");
        }

        private void InsertSentence(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("s");
        }

        private void InsertSayAs(object sender, RoutedEventArgs e)
        {
            var item = e.OriginalSource as RibbonMenuItem;
            if (item == null) return;
            InsertXmlElement("say-as", " interpret-as=\"" + item.Tag + "\"");
        }

        private void InsertPhoneme(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("phoneme", " ph=\"\"", true);
        }

        private void InsertSub(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("sub", " alias=\"\"", true);
        }

        private void InsertVoice(object sender, RoutedEventArgs e)
        {
            var item = SetVoiceButton.ItemContainerGenerator.ItemFromContainer((DependencyObject)e.OriginalSource) as VoiceInfo;
            if (item == null) return;
            InsertXmlElement("voice", " name=\"" + item.Name + "\"");
        }

        private void InsertEmphasis(object sender, RoutedEventArgs e)
        {
            var item = e.OriginalSource as RibbonMenuItem;
            if (item == null) return;
            InsertXmlElement("emphasis", " level=\"" + item.Tag + "\"");
        }

        private void InsertBreak(object sender, RoutedEventArgs e)
        {
            InsertSelfCloseXmlElement("break", " time=\"\"", true);
        }

        private void InsertProsodyPitch(object sender, RoutedEventArgs e)
        {
            var item = e.OriginalSource as RibbonMenuItem;
            if (item == null) return;
            InsertXmlElement("prosody", " pitch=\"" + item.Tag + "\"");
        }

        private void InsertProsodyContour(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("prosody", " contour=\"\"", true);
        }

        private void InsertProsodyRate(object sender, RoutedEventArgs e)
        {
            var item = e.OriginalSource as RibbonMenuItem;
            if (item == null) return;
            InsertXmlElement("prosody", " rate=\"" + item.Tag + "\"");
        }

        private void InsertProsodyDuration(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("prosody", " duration=\"\"", true);
        }

        private void InsertProsodyVolume(object sender, RoutedEventArgs e)
        {
            var item = e.OriginalSource as RibbonMenuItem;
            if (item == null) return;
            InsertXmlElement("prosody", " volume=\"" + item.Tag + "\"");
        }

        private void InsertProsodyVolumeCustom(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("prosody", " volume=\"\"", e.Handled = true);
        }

        private void InsertProsodyPitchCustom(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("prosody", " pitch=\"Hz\"", e.Handled = true);
        }

        private void InsertProsodyRateCustom(object sender, RoutedEventArgs e)
        {
            InsertXmlElement("prosody", " rate=\"\"", e.Handled = true);
        }

        private void InsertAudio(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrEmpty(EnterTextBox.SelectedText)) InsertSelfCloseXmlElement("audio", " src=\"\"", true);
            else InsertXmlElement("audio", " src=\"\"", true);
        }

        private void InsertPath(object sender, RoutedEventArgs e)
        {
            if (openAudioDialog.ShowDialog() == true) ReplaceSelection(openAudioDialog.FileName);
        }

        private void VistaOrUpper(object sender, CanExecuteRoutedEventArgs e)
        {
            e.CanExecute = Environment.OSVersion.Version.Major >= 6;
        }
    }

    internal static class Commands
    {
        public static readonly RoutedCommand Synthesize = new RoutedCommand
            { InputGestures = { new KeyGesture(Key.Enter, ModifierKeys.Control) } }, Empty = new RoutedCommand(),
            ConvertSsml = new RoutedCommand { InputGestures = { new KeyGesture(Key.F11) } };
    }

    [ValueConversion(typeof(int), typeof(double))]
    public class KiloBytesConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return ((int)value) / 1024.0;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return (int)(((double)value) * 1024);
        }
    }

    [ValueConversion(typeof(VoiceGender), typeof(string))]
    public class GenderConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            switch ((VoiceGender)value)
            {
                case VoiceGender.Male:
                    return "男性";
                case VoiceGender.Female:
                    return "女性";
                case VoiceGender.Neutral:
                    return "中性";
                default:
                    return "未知";
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }

    [ValueConversion(typeof(VoiceAge), typeof(string))]
    public class AgeConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            switch ((VoiceAge)value)
            {
                case VoiceAge.Senior:
                    return "老人";
                case VoiceAge.Adult:
                    return "成人";
                case VoiceAge.Teen:
                    return "青年";
                case VoiceAge.Child:
                    return "孩子";
                default:
                    return "未知";
            }
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotSupportedException();
        }
    }

    [ValueConversion(typeof(bool?), typeof(bool?))]
    public class NotConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value == null || !(value is bool?)) return null;
            var b = (bool?)value;
            return !b;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return Convert(value, targetType, parameter, culture);
        }
    }

    [ValueConversion(typeof(RibbonCheckBox), typeof(bool?))]
    public class EnabledAndCheckedConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            var box = value as RibbonCheckBox;
            if (box == null) return null;
            return box.IsChecked != null && box.IsEnabled && box.IsChecked.Value;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return new NotSupportedException();
        }
    }

    [ValueConversion(typeof(bool?), typeof(Visibility))]
    public class VisibleWhileTrueConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value == null || !(value is bool?)) return null;
            var b = (bool?)value;
            return b == true ? Visibility.Visible : Visibility.Collapsed;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return new NotSupportedException();
        }
    }
}
