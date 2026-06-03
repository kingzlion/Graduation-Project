import os
import shapefile

xa_dir = "/Users/zli/Documents/gs/Graduation-Project/gis-data"

print("================================================================")
print("Scanning ESRI Shapefiles with GBK encoding in xa/ folder...")
print("================================================================")

for root, dirs, files in os.walk(xa_dir):
    for f in files:
        if f.endswith(".shp"):
            shp_path = os.path.join(root, f)
            size_mb = os.path.getsize(shp_path) / (1024 * 1024)
            print(f"\n📁 File: {f} ({size_mb:.2f} MB)")
            
            try:
                # Specify encoding='gbk' for Chinese shapefiles
                sf = shapefile.Reader(shp_path, encoding="gbk")
                print(f"   Geometry Type: {sf.shapeTypeName} (Type code: {sf.shapeType})")
                print(f"   Record Count : {len(sf)}")
                
                # Print fields
                fields = [field[0] for field in sf.fields if field[0] != "DeletionFlag"]
                print(f"   Fields       : {fields}")
                
                # Print first record as sample
                if len(sf) > 0:
                    first_record = sf.record(0).as_dict()
                    # Truncate values for readability
                    sample = {k: (str(v)[:40] + "...") if len(str(v)) > 40 else v for k, v in first_record.items()}
                    print(f"   Sample Rec[0]: {sample}")
            except Exception as e:
                print(f"   ❌ Error reading file: {e}")

print("\n================================================================")
