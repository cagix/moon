(ns cdq.graphics)

(defmulti draw!
  (fn [[k] _graphics]
    k))

; shape-drawer
; batch
; textures
; unit-scale
; world-unit-scale
; default-font
; do select-keys?
; or new namespace?

(defn handle-draws! [graphics draws]
  (doseq [component draws
          :when component]
    (draw! component graphics)))
