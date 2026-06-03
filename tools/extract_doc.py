import sys

def extract_text(filename):
    with open(filename, 'rb') as f:
        data = f.read()

    # Try to extract GBK and UTF-16LE strings (heuristically)
    # Actually, a simpler way is just replacing non-printable chars or using 'strings' command with -e l (UTF-16) or -e S (GBK is not supported directly by strings, but we can try).
    
    # In python, let's just ignore errors and decode blocks.
    # .doc text streams are often in UTF-16LE
    try:
        text = data.decode('utf-16le', errors='ignore')
        # Filter out garbage
        filtered = ''.join(c for c in text if c.isprintable() or c in '\n\r\t')
        print(filtered[:5000]) # Print first 5000 chars to see if we got something
    except Exception as e:
        print(e)

extract_text("20170813036_王自立_基于WebGIS的雄安新区建设成果展示系统的设计与实现.doc")
