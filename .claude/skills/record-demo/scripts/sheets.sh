#!/usr/bin/env bash
# sheets.sh <video.mp4> <outdir> [fps=1]
# Contact sheets from a clip: 5x2 tiles, 300 px wide each, labelled with the clip name and timestamp.
# Prints how many sheets it wrote. Needs ffmpeg (with drawtext).
set -eu
[ $# -ge 2 ] || { echo "usage: $0 <video.mp4> <outdir> [fps=1]" >&2; exit 2; }
v=$1 o=$2 fps=${3:-1}
mkdir -p "$o"
n=$(basename "$v" .mp4)
ffmpeg -loglevel error -y -i "$v" \
  -vf "fps=$fps,scale=300:-1,drawtext=text='$n %{pts\\:hms}':x=4:y=4:fontsize=14:fontcolor=white:box=1:boxcolor=black@0.6,tile=5x2:padding=4:color=gray" \
  "$o/${n}_%02d.png"
ls "$o" | grep -c "^${n}_"
