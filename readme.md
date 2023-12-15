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

## 外部工具

1. 安装 [llama factory](https://github.com/hiyouga/LLaMA-Factory/)
2. 安装 [llama.cpp](https://github.com/ggerganov/llama.cpp)
3. 安装 [ollama](https://ollama.ai/)
4. 安装 [blue-shell](https://github.com/MarchLiu/blue-shell)
5. 安装 jdk21

## 训练和使用

1. 将 staff 目录下的 export.sh 和 train.sh 复制到 llama factory 目录。
2. 修改文件内容使关键路径符合你的环境。如果不需要像我一样基于本地的模型文件，可以自行修改为符合 huggingface 或 modelscope 的标识。
3. 先按照 lora-data-generator 的说明构建程序，生成训练需要的文件
4. 在 llama factory 目录下执行 `./train.sh` 生成预训练文件
5. 在 llama factory 目录下执行 `./export.sh` 生成导出的模型
6. 按 llama.cpp 的文档生成可以为 ollama 使用的 `.gguf` 文件 `python convert.py ~/jobs/llm/oliva/model_export`。
7. 按 ollama 的文档创建 ollama 模型 `ollama create oliva -f ./Modelfile`
8. 此时 ollama 服务应该已经在运行，启动 blue shell，即可使用自己定制的模型了。`python -m blueshell.shell -m oliva  -f markdown`