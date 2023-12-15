#!/usr/bin/env sh

# place in llama factory directory

python src/train_bash.py \
    --stage pt \
    --model_name_or_path /Users/mars/jobs/llm/CodeLlama-7B \
    --do_train \
    --dataset oliva \
    --finetuning_type lora \
    --lora_target q_proj,v_proj \
    --output_dir /Users/mars/jobs/llm/oliva/checkpoint \
    --overwrite_cache \
    --per_device_train_batch_size 4 \
    --gradient_accumulation_steps 4 \
    --lr_scheduler_type cosine \
    --logging_steps 10 \
    --save_steps 1000 \
    --learning_rate 5e-5 \
    --num_train_epochs 3.0 \
    --plot_loss

