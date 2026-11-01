# Yoii özel 3B model formatı

Dosyaları oyun klasöründeki `kuro_yoii` klasörüne koy, menüden "Skin klasörünü yenile"ye bas.

- `isim.png` → normal skin (64x64).
- `model_xxx.png` → bir modelin kendi dokusu (tek başına skin sayılmaz).
- `isim.json` → karakter: skin + 3B ek geometri.

```json
{
  "skin": "benim_skinim.png",
  "texture": "model_xxx.png",
  "tex_w": 128, "tex_h": 128,
  "parts": [
    { "bone": "body", "pivot": [0, 0, 0], "rot": [0, 0, 0],
      "cubes": [ { "o": [-5, 8, -3], "s": [10, 5, 6], "uv": [0, 0], "d": 0.0 } ] }
  ]
}
```

- `bone`: head, body, left_arm, right_arm, left_leg, right_leg (parça bu kemikle birlikte hareket eder).
- `pivot`: kemiğe göre dönüş noktası, `rot`: derece cinsinden dönüş (x, y, z).
- Kutu: `o` başlangıç noktası, `s` boyut (w, h, d), `uv` doku başlangıcı, `d` şişirme (dokuyu germeden büyütür).
- `"texture": "skin"` yazarsan (ya da hiç yazmazsan) parça oyuncunun skin dokusunu kullanır.
- Vanilla ölçüleri: gövde x -4..4, y 0..12, z -2..2; kol/bacak 4x12x4, kafa y -8..0.
