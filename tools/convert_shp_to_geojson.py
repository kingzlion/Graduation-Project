import os
import json
import shapefile

xa_dir = "/Users/zli/Documents/gs/Graduation-Project/gis-data"
static_geojson_dir = "/Users/zli/Documents/gs/Graduation-Project/frontend/src/static/geojson"

# Ensure output directory exists
os.makedirs(static_geojson_dir, exist_ok=True)

def shapefile_to_geojson(shp_name, encoding='gbk'):
    shp_path = os.path.join(xa_dir, shp_name + ".shp")
    if not os.path.exists(shp_path):
        # Retry with lowercase or search recursively
        found = False
        for root, dirs, files in os.walk(xa_dir):
            if shp_name + ".shp" in files:
                shp_path = os.path.join(root, shp_name + ".shp")
                found = True
                break
        if not found:
            print(f"❌ File not found: {shp_name}.shp")
            return
            
    json_path = os.path.join(static_geojson_dir, shp_name + ".json")
    
    try:
        sf = shapefile.Reader(shp_path, encoding=encoding)
        features = []
        for shape_rec in sf.shapeRecords():
            geom = shape_rec.shape.__geo_interface__
            rec = shape_rec.record.as_dict()
            
            feature = {
                "type": "Feature",
                "geometry": geom,
                "properties": rec
            }
            features.append(feature)
            
        geojson = {
            "type": "FeatureCollection",
            "features": features
        }
        
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(geojson, f, ensure_ascii=False, indent=2)
        print(f"✅ Successfully converted {shp_name}.shp -> {shp_name}.json ({len(features)} features)")
    except Exception as e:
        print(f"❌ Error converting {shp_name}.shp: {e}")

# Convert key shapefiles
shapefile_to_geojson("xianjie")
shapefile_to_geojson("quanjie")
shapefile_to_geojson("xioan")
shapefile_to_geojson("xinan")
shapefile_to_geojson("baodingshi")
