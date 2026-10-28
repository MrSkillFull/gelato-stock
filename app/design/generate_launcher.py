"""Convert the approved, unchanged SVGs to Android vectors (Python stdlib only).

Run from any directory: python app/design/generate_launcher.py
Only the SVG subset used by these three assets is accepted.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
RES = HERE.parent / 'src/main/res'
NS = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', NS)


def attrs(**values):
    return {f'{{{NS}}}{k}': str(v) for k, v in values.items()}


def convert_node(src, parent, inherited=None):
    tag = src.tag.split('}')[-1]
    if tag in ('title', 'desc'):
        return
    style = dict(inherited or {})
    style.update({k: v for k, v in src.attrib.items() if k != 'transform'})
    for operation, arguments in re.findall(r'(\w+)\(([^)]+)\)', src.get('transform', '')):
        v = [float(x) for x in re.split(r'[ ,]+', arguments.strip())]
        if operation == 'translate':
            group = attrs(translateX=v[0], translateY=v[1] if len(v) > 1 else 0)
        elif operation == 'scale':
            group = attrs(scaleX=v[0], scaleY=v[1] if len(v) > 1 else v[0])
        elif operation == 'rotate' and len(v) == 1:
            group = attrs(rotation=v[0])
        else:
            raise ValueError(f'Unsupported transform: {operation} {v}')
        parent = ET.SubElement(parent, 'group', group)
    if tag in ('svg', 'g'):
        for child in src:
            convert_node(child, parent, style)
        return
    if tag == 'path':
        data = src.attrib['d']
    elif tag == 'rect':
        w, h, r = (float(src.get(k, '0')) for k in ('width', 'height', 'rx'))
        data = f'M{r},0 H{w-r} A{r},{r} 0 0 1 {w},{r} V{h-r} A{r},{r} 0 0 1 {w-r},{h} H{r} A{r},{r} 0 0 1 0,{h-r} V{r} A{r},{r} 0 0 1 {r},0 Z'
    elif tag == 'ellipse':
        x, y, rx, ry = (float(src.attrib[k]) for k in ('cx', 'cy', 'rx', 'ry'))
        data = f'M{x-rx},{y} A{rx},{ry} 0 1 0 {x+rx},{y} A{rx},{ry} 0 1 0 {x-rx},{y} Z'
    else:
        raise ValueError(f'Unsupported SVG element: {tag}')
    out = attrs(pathData=data, fillColor=style.get('fill', '#000000').replace('none', '#00000000'))
    if 'opacity' in style:
        out.update(attrs(fillAlpha=style['opacity']))
    mapping = {'stroke': 'strokeColor', 'stroke-width': 'strokeWidth',
               'stroke-opacity': 'strokeAlpha', 'stroke-linecap': 'strokeLineCap',
               'stroke-linejoin': 'strokeLineJoin'}
    for source, target in mapping.items():
        if source in style:
            out.update(attrs(**{target: style[source].replace('none', '#00000000')}))
    ET.SubElement(parent, 'path', out)


def write(root, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(root, space='    ')
    path.write_text(ET.tostring(root, encoding='unicode', xml_declaration=True) + '\n', encoding='utf-8')


def vector(source, target, size):
    svg = ET.parse(HERE / source).getroot()
    _, _, w, h = svg.attrib['viewBox'].split()
    root = ET.Element('vector', attrs(width=f'{size}dp', height=f'{size}dp', viewportWidth=w, viewportHeight=h))
    parent = root
    if size == 108:
        # Uniform 7% reduction: original artwork radius 35.18dp exceeds
        # Android's 33dp safe circle. Keep the approved source unchanged.
        parent = ET.SubElement(root, 'group', attrs(scaleX='.93', scaleY='.93', pivotX=54, pivotY=54))
    convert_node(svg, parent)
    write(root, RES / target)


if __name__ == '__main__':
    vector('gelatostock_icon.svg', 'mipmap-anydpi/ic_launcher.xml', 48)
    vector('gelatostock_icon.svg', 'mipmap-anydpi/ic_launcher_round.xml', 48)
    vector('gelatostock_foreground.svg', 'drawable/ic_launcher_foreground.xml', 108)
    vector('gelatostock_monochrome.svg', 'drawable/ic_launcher_monochrome.xml', 108)
    background = ET.Element('vector', attrs(width='108dp', height='108dp', viewportWidth=108, viewportHeight=108))
    ET.SubElement(background, 'path', attrs(fillColor='#FFF8ED', pathData='M0,0 H108 V108 H0 Z'))
    write(background, RES / 'drawable/ic_launcher_background.xml')
    for name in ('ic_launcher', 'ic_launcher_round'):
        root = ET.Element('adaptive-icon')
        for layer in ('background', 'foreground', 'monochrome'):
            ET.SubElement(root, layer, attrs(drawable=f'@drawable/ic_launcher_{layer}'))
        write(root, RES / f'mipmap-anydpi-v26/{name}.xml')
