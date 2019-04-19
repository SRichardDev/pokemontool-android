#!/bin/bash

input_dir=$(pwd)/../models/official-artwork
output_dir=$(pwd)/app/src/main/assets

mkdir -p ${output_dir}

### Examples for different conversions:

### white in black
# convert 1.png -alpha extract -threshold 30% 1_white.png

### black in white
# convert 1.png -alpha extract -negate -threshold 30% 1_black.png

### black outline
# convert 1.png -alpha extract -edge 2 -negate 1_outline.png

for image in ${input_dir}/*.png; do
	image_name=$(basename "$image" .png)
	convert ${image} -alpha extract -edge 2 -negate ${output_dir}/"$image_name".png
done

