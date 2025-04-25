(ns cdq.graphics)

(defprotocol Graphics
  (world-mouse-position [_])
  (set-cursor! [_ cursor-key])
  (set-camera-position! [_ [x y]])
  (increment-zoom! [_ pos-or-neg-number])
  (camera-position [_])
  (camera-zoom [_])
  (draw-on-world-view! [_ render-fn])
  (on-resize! [_ width height])
  (ui-mouse-position [_])
  (ui-viewport-width [_])
  (ui-viewport-height [_])
  ; TODO 'cdq.line-of-sight/on-screen?' breaks application/domain logic

  ; TODO shape-drawer render fns

  ; TODO gdl.graphics px->wu etc.

  ; batch draw sprites render fns

  ; TODO text draw fns

  ; TODO sprite create ... but also requires assets ... ??? -> Textures part of graphics ???
  ; how to manage sounds ?
  )
