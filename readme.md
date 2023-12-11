# Oliva 

Oliva 是一个 AI 和模型仓库，我会将一些实验代码保存在这里。

## lora-data-generator

lora-data-generator 是一个 lora 数据集生成工具，识别一些编程语言的
代码（目前支持c和java），然后生成为 lora 训练数据集。

这里面用到了组合子库 jaskell-rocks，编写了一个非常简单的词法分析器。
还用到了其中的 ArgParser 处理命令行参数。正常来说 Java 项目可以用
apache commons-cli 。但是 Jaskell 中的这个版本更能满足我的需求，
特别是与 Jaskell 中其它函数式编程设计结合的更好。

这个项目用 Java 21开发，并且用到了预览版本的语言功能，所以如果在本地
运行，需要安装Java 21 环境，并且给 java 加上 `--enable-preview` 
参数。

基本的用法类似这样：

```shell
java --enable-preview --source /data/project0 --source /data/project1 --target /data/llm/trans_data.json 
```

可以通过多次设定 `--source` 参数传入多个项目的路径，最终它们的信息会统一写到 `--target` 所指定的文件。