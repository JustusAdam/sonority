set -e

OUTPUT_DIR="app"
SOURCE_DIR="src"

if [ ! -d "$OUTPUT_DIR" ]; then
  mkdir "$OUTPUT_DIR"
fi


coffee -c -o "$OUTPUT_DIR" "$SOURCE_DIR"
