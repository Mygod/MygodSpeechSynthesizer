﻿//------------------------------------------------------------------------------
// <auto-generated>
//     此代码由工具生成。
//     运行时版本:4.0.30319.34003
//
//     对此文件的更改可能会导致不正确的行为，并且如果
//     重新生成代码，这些更改将会丢失。
// </auto-generated>
//------------------------------------------------------------------------------

namespace Mygod.Speech.Synthesizer.Properties {
    using System;
    
    
    /// <summary>
    ///   一个强类型的资源类，用于查找本地化的字符串等。
    /// </summary>
    // 此类是由 StronglyTypedResourceBuilder
    // 类通过类似于 ResGen 或 Visual Studio 的工具自动生成的。
    // 若要添加或移除成员，请编辑 .ResX 文件，然后重新运行 ResGen
    // (以 /str 作为命令选项)，或重新生成 VS 项目。
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("System.Resources.Tools.StronglyTypedResourceBuilder", "4.0.0.0")]
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    internal class Resources {
        
        private static global::System.Resources.ResourceManager resourceMan;
        
        private static global::System.Globalization.CultureInfo resourceCulture;
        
        [global::System.Diagnostics.CodeAnalysis.SuppressMessageAttribute("Microsoft.Performance", "CA1811:AvoidUncalledPrivateCode")]
        internal Resources() {
        }
        
        /// <summary>
        ///   返回此类使用的缓存的 ResourceManager 实例。
        /// </summary>
        [global::System.ComponentModel.EditorBrowsableAttribute(global::System.ComponentModel.EditorBrowsableState.Advanced)]
        internal static global::System.Resources.ResourceManager ResourceManager {
            get {
                if (object.ReferenceEquals(resourceMan, null)) {
                    global::System.Resources.ResourceManager temp = new global::System.Resources.ResourceManager("Mygod.Speech.Synthesizer.Properties.Resources", typeof(Resources).Assembly);
                    resourceMan = temp;
                }
                return resourceMan;
            }
        }
        
        /// <summary>
        ///   使用此强类型资源类，为所有资源查找
        ///   重写当前线程的 CurrentUICulture 属性。
        /// </summary>
        [global::System.ComponentModel.EditorBrowsableAttribute(global::System.ComponentModel.EditorBrowsableState.Advanced)]
        internal static global::System.Globalization.CultureInfo Culture {
            get {
                return resourceCulture;
            }
            set {
                resourceCulture = value;
            }
        }
        
        /// <summary>
        ///   查找类似 &lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt;
        ///&lt;speak version=&quot;1.0&quot; xmlns=&quot;http://www.w3.org/2001/10/synthesis&quot; xml:lang=&quot;zh-CN&quot;&gt;
        ///  &lt;voice xml:lang=&quot;zh-CN&quot; xmlns:xsi=&quot;http://www.w3.org/2001/XMLSchema-instance&quot; xsi:schemaLocation=&quot;http://www.w3.org/2001/10/synthesis&amp;#xD;&amp;#xA;                   http://www.w3.org/TR/speech-synthesis/synthesis.xsd&quot;&gt;
        ///    &lt;p&gt;
        ///      &lt;s&gt;请在这里输入一句句子。&lt;/s&gt;
        ///    &lt;/p&gt;
        ///  &lt;/voice&gt;
        ///&lt;/speak&gt; 的本地化字符串。
        /// </summary>
        internal static string DefaultSSML {
            get {
                return ResourceManager.GetString("DefaultSSML", resourceCulture);
            }
        }
        
        /// <summary>
        ///   查找类似 ID：{0}
        ///名称：{1}
        ///声音：{2}
        ///年龄：{3}
        ///语言 (国家)：{4}
        ///描述：{5} 的本地化字符串。
        /// </summary>
        internal static string VoiceDetails {
            get {
                return ResourceManager.GetString("VoiceDetails", resourceCulture);
            }
        }
        
        /// <summary>
        ///   查找类似 &lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt;
        ///&lt;speak version=&quot;1.0&quot; xmlns=&quot;http://www.w3.org/2001/10/synthesis&quot; xml:lang=&quot;zh-CN&quot;&gt;
        ///  &lt;p&gt;
        ///    &lt;s&gt;欢迎使用 &lt;sub alias=&quot;My god&quot;&gt;Mygod&lt;/sub&gt;语音合成器 V{0}！&lt;/s&gt;
        ///    &lt;s&gt;本程序由 &lt;sub alias=&quot;My god&quot;&gt;Mygod&lt;/sub&gt; 于 &lt;say-as interpret-as=&quot;date:ymd&quot;&gt;{1}&lt;/say-as&gt;&lt;say-as interpret-as=&quot;time:hms&quot;&gt;{2}&lt;/say-as&gt; 制作。&lt;/s&gt;
        ///    &lt;s&gt;了解更多关于本程序的信息或查找更新请访问：&lt;say-as interpret-as=&quot;net:uri&quot;&gt;http://mygodstudio.tk/product/mygod-speech-synthesizer/&lt;/say-as&gt;。&lt;/s&gt;
        ///  &lt;/p&gt;
        ///  &lt;p&gt;
        ///    &lt;s&gt;快速帮助：请在这里禁用 SSML 后输入你要阅读的 [字符串的其余部分被截断]&quot;; 的本地化字符串。
        /// </summary>
        internal static string WelcomingText {
            get {
                return ResourceManager.GetString("WelcomingText", resourceCulture);
            }
        }
    }
}