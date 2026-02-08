#!/usr/bin/env python3
"""
Train a YOLOv8n model for dart detection and export to ONNX.

Classes (5):
  0: cal_20  - Calibration point at segment 20 (top)
  1: cal_3   - Calibration point at segment 3 (bottom)
  2: cal_11  - Calibration point at segment 11 (left)
  3: cal_6   - Calibration point at segment 6 (right)
  4: dart    - Dart tip

Usage:
  pip install ultralytics roboflow
  python scripts/train_model.py
  python scripts/train_model.py --dataset-path /path/to/dataset
  python scripts/train_model.py --epochs 200 --batch 16

The exported model (dart_detect.onnx) should be placed in:
  composeApp/src/androidMain/assets/dart_detect.onnx
"""

import argparse
import shutil
from pathlib import Path


def download_dataset(output_dir: Path) -> Path:
    """Download dart detection dataset from Roboflow."""
    try:
        from roboflow import Roboflow
    except ImportError:
        print("ERROR: roboflow package not installed. Run: pip install roboflow")
        print("Alternatively, provide a local dataset with --dataset-path")
        raise SystemExit(1)

    rf = Roboflow()
    print("Log in to Roboflow when prompted to download the dataset.")
    print("You can also set the ROBOFLOW_API_KEY environment variable.")
    project = rf.workspace().project("dart-detection")
    version = project.version(1)
    dataset = version.download("yolov8", location=str(output_dir / "dataset"))
    return Path(dataset.location)


def train(dataset_path: Path, epochs: int, batch: int, imgsz: int) -> Path:
    """Train YOLOv8n and return the path to the best weights."""
    try:
        from ultralytics import YOLO
    except ImportError:
        print("ERROR: ultralytics package not installed. Run: pip install ultralytics")
        raise SystemExit(1)

    model = YOLO("yolov8n.pt")
    data_yaml = dataset_path / "data.yaml"

    if not data_yaml.exists():
        print(f"ERROR: data.yaml not found at {data_yaml}")
        print("Ensure your dataset has YOLO format with a data.yaml file.")
        raise SystemExit(1)

    print(f"Training YOLOv8n on {data_yaml}")
    print(f"  epochs={epochs}, batch={batch}, imgsz={imgsz}")

    results = model.train(
        data=str(data_yaml),
        epochs=epochs,
        batch=batch,
        imgsz=imgsz,
        project="runs/dart_detect",
        name="train",
        exist_ok=True,
    )

    best_weights = Path("runs/dart_detect/train/weights/best.pt")
    if not best_weights.exists():
        print(f"ERROR: Training completed but best.pt not found at {best_weights}")
        raise SystemExit(1)

    print(f"Training complete. Best weights: {best_weights}")
    return best_weights


def export_onnx(weights_path: Path, imgsz: int, output_path: Path) -> None:
    """Export trained model to ONNX format."""
    from ultralytics import YOLO

    model = YOLO(str(weights_path))
    model.export(format="onnx", imgsz=imgsz, simplify=True)

    onnx_path = weights_path.with_suffix(".onnx")
    if not onnx_path.exists():
        print(f"ERROR: ONNX export failed, file not found at {onnx_path}")
        raise SystemExit(1)

    shutil.copy2(onnx_path, output_path)
    print(f"ONNX model exported to: {output_path}")
    print(f"Copy to: composeApp/src/androidMain/assets/dart_detect.onnx")


def main() -> None:
    parser = argparse.ArgumentParser(description="Train YOLOv8n dart detector")
    parser.add_argument(
        "--dataset-path",
        type=Path,
        default=None,
        help="Path to local dataset (YOLO format with data.yaml). "
        "If not provided, downloads from Roboflow.",
    )
    parser.add_argument("--epochs", type=int, default=100, help="Training epochs")
    parser.add_argument("--batch", type=int, default=16, help="Batch size")
    parser.add_argument("--imgsz", type=int, default=640, help="Image size")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("dart_detect.onnx"),
        help="Output ONNX file path",
    )
    args = parser.parse_args()

    if args.dataset_path is None:
        dataset_path = download_dataset(Path("runs"))
    else:
        dataset_path = args.dataset_path

    best_weights = train(dataset_path, args.epochs, args.batch, args.imgsz)
    export_onnx(best_weights, args.imgsz, args.output)

    print("\nDone! Next steps:")
    print(f"  1. Copy {args.output} to composeApp/src/androidMain/assets/dart_detect.onnx")
    print("  2. Build the app: ./gradlew :composeApp:assembleDebug")
    print("  3. Open Vision Debug Screen to test detections")


if __name__ == "__main__":
    main()
