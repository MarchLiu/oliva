#!/usr/bin/env sh
# place in llama factory directory

python src/export_model.py \
    --model_name_or_path /Users/mars/jobs/llm/CodeLlama-7B \
    --template default \
    --finetuning_type lora \
    --checkpoint_dir /Users/mars/jobs/llm/oliva/checkpoint/ \
    --export_dir /Users/mars/jobs/llm/oliva/model_export
