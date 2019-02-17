#!/bin/bash

source_path="/Users/steven/SRC/pokemontool-ios/PokemonGoTool/PokemonGoTool/Assets.xcassets"
assets_path="/Users/steven/SRC/pokemontool-android/app/src/main/assets"
drawables_path="/Users/steven/SRC/pokemontool-android/app/src/main/res/drawable"
path_svg2vector_tool="/Users/steven/SRC/tools/Svg2VectorAndroid-1.0.jar"


### copy and convert pdf to svg
find "$source_path" -type f  -name "*.pdf" -exec cp -i {} "$assets_path"  \;

for file in "$assets_path"/*.pdf; do
    basename=$(basename "$file" .pdf)
    pdf2svg "$assets_path"/"$basename".pdf "$assets_path"/"$basename".svg
done

### clean up
rm -f "$assets_path"/*.pdf

### remove all old "_svg.xml" files in drawables
for file in "$drawables_path"/*.xml; do
    if [[ $file == *"_svg.xml" ]]; then
        rm -f "$file"
    fi
done

### convert all svg in xml and mv to drawables directory
java -jar "$path_svg2vector_tool" "$assets_path"
processed_svg_path="$assets_path"/ProcessedSVG

# rename all .xml to lovercase
for f in "$processed_svg_path"/*.xml; do
    mv "$f" "$f.tmp"; mv "$f.tmp" "`echo $f | tr "[:upper:]" "[:lower:]"`";
done

find "$assets_path" -type f -name "*.xml" -exec mv {} "$drawables_path"  \;

### clean up
rm -rf "$processed_svg_path"


### TODOs
### copy appicon png to androids directory structur

### for multiple names to move
#find "$source_path" -type f \( -name "*.png" -o -name "*.pdf" \) -exec mv -i {} "$assets_path"  \;
