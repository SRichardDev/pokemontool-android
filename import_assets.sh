#!/bin/bash

source_path="/Users/steven/SRC/pokemontool-ios/PokemonGoTool/PokemonGoTool/Assets.xcassets"
target_path="/Users/steven/SRC/pokemontool-android/app/src/main/assets"

### for multiple names to move
#find "$source_path" -type f \( -name "*.png" -o -name "*.pdf" \) -exec mv -i {} "$target_path"  \;

### copy and convert pdf to svg
find "$source_path" -type f  -name "*.pdf" -exec cp -i {} "$target_path"  \;

for file in "$target_path"/*.pdf; do
    basename=$(basename "$file" .pdf)
    pdf2svg "$target_path"/"$basename".pdf "$target_path"/"$basename".svg
done

rm -f "$target_path"/*.pdf

### copy appicon png to androids directory structur

