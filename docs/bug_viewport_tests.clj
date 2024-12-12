
(defn world-mouse-position []
  ; TODO clamping only works for gui-viewport ? check. comment if true
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject-mouse-position world-viewport))
; TODO was 'viewport' not world
; how could I have found this bug? => tests .... for world-mouse-poisition
; expecting a certain point ....
