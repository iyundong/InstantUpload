# iPhone的支持

我们目前的传输来看，安卓手机基本上使用的是OTG线与相机进行连接的，但是对于iPhone来说，实现OTG连接有一定难度，我们看iPhone的连接接口，是苹果的Lightning接口，Lightning接口可以看如下的介绍：

- <https://baike.baidu.com/item/Lightning/3164411?fr=aladdin>
- <http://www.pc841.com/shoujizhishi/67063_2.html>
- <https://www.zhihu.com/question/24845265>

Lightning接口和USB还是有较大区别的

```
作者：路痴：https://www.zhihu.com/question/24845265/answer/29188584
Lightning和USB不是同一个层面的东西。认为两者只差一个双面插入就错了。从theiphonewiki Lightning Connector 里这段匿名懦夫（很可能是个apple工程师）爆料来看，Lightning的设计是：让线智能化，线里面必须有个主控芯片用来和iDevice交互。然后通过线跟主机交互的过程分别是：从iDevice->线主控，不知名高速串行总线；从线主控到PC，USB2.0（当前——我是说截至2014.8）。这个设计的好处不言而喻，iDevice成为真正意义上的主机（不是USB otg那种首鼠两端的），理论上可以输入/输出到任意设备（还记得之前听说的Lightning耳机么？）。就数据线而言，虽然当前只有USB 2.0，但以后换成USB3.0/3.1很可能也只是换根线的问题。（未考虑 @时国怀 提到的物理设计问题。但iPhone5发布时USB3.0已出，我估计苹果应该不至于没考虑过这类问题吧）坏处当然是由于线并非dumb线，成本提高（不过这个难题已被国内山寨厂率先攻克），另外输出HDMI这类数据时必须先在iDevice端编码，线主控再解码成HDMI数据流，这也就是之前Panic发现iPhone5的HDMI输出线里居然塞进了一台iPhone4的CPU的原因，延迟不说，画质也因此受到影响。而USB，不管它是什么版本……都是dumb线。
Lightning和USB不是同一个层面的东西。认为两者只差一个双面插入就错了。从theiphonewiki Lightning Connector 里这段匿名懦夫（很可能是个apple工程师）爆料来看，Lightning的设计是：让线智能化，线里面必须有个主控芯片用来和iDevice交互。然后通过线跟主机交互的过程分别是：从iDevice->线主控，不知名高速串行总线；从线主控到PC，USB2.0（当前——我是说截至2014.8）。这个设计的好处不言而喻，iDevice成为真正意义上的主机（不是USB otg那种首鼠两端的），理论上可以输入/输出到任意设备（还记得之前听说的Lightning耳机么？）。就数据线而言，虽然当前只有USB 2.0，但以后换成USB3.0/3.1很可能也只是换根线的问题。（未考虑 @时国怀 提到的物理设计问题。但iPhone5发布时USB3.0已出，我估计苹果应该不至于没考虑过这类问题吧）坏处当然是由于线并非dumb线，成本提高（不过这个难题已被国内山寨厂率先攻克），另外输出HDMI这类数据时必须先在iDevice端编码，线主控再解码成HDMI数据流，这也就是之前Panic发现iPhone5的HDMI输出线里居然塞进了一台iPhone4的CPU的原因，延迟不说，画质也因此受到影响。而USB，不管它是什么版本……都是dumb线。
```

可见，苹果的Lightning数据线和USB设备相对，多出了一个MFI认证层，这里面，苹果在商业上限制了只有经过MFI认证的设备才可以与之连接传输，虽然苹果有自己的USB Host设备，并切Lightning的传输线缆部分与USB设备并无却别，但是这层MFI认证让第三方USB设备的接入增加很大的难度..
