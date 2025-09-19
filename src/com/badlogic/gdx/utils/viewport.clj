(ns com.badlogic.gdx.utils.viewport
  (:require [clojure.utils :as utils]
            [gdl.graphics.viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils.viewport Viewport
                                            FitViewport)))

(defn- unproject [viewport x y]
  (let [vector2 (.unproject viewport (Vector2. x y))]
    [(.x vector2)
     (.y vector2)]))

(extend-type Viewport
  gdl.graphics.viewport/Viewport
  (update! [this width height {:keys [center?]}]
    (.update this width height center?))

  ; touch coordinates are y-down, while screen coordinates are y-up
  ; so the clamping of y is reverse, but as black bars are equal it does not matter
  ; TODO clamping only works for gui-viewport ?
  ; TODO ? "Can be negative coordinates, undefined cells."
  (unproject [this [x y]]
    (unproject this
               (utils/clamp x
                            (:viewport/left-gutter-width this)
                            (:viewport/right-gutter-x    this))
               (utils/clamp y
                            (:viewport/top-gutter-height this)
                            (:viewport/top-gutter-y      this)))))

; TODO where does this come from ???
; Where do I pass it ?
; -> to Stage ..
; -> what does it do with it?
; -> interface?
(comment
 :graphics/ui-viewport
 :graphics/world-viewport

 ; and in levelgen:
 ; passed to stage!
 )
; FitViewport extends ScalingViewport
; ScalingViewport extends Viewport
; Options to go deeper:
; * extend ScalingViewport yourself
; write yourself
; or extract interfce viewport and implement with atoms/state
(defn fit [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (case k
        :viewport/width             (FitViewport/.getWorldWidth      this)
        :viewport/height            (FitViewport/.getWorldHeight     this)
        :viewport/camera            (FitViewport/.getCamera          this)
        :viewport/left-gutter-width (FitViewport/.getLeftGutterWidth this)
        :viewport/right-gutter-x    (FitViewport/.getRightGutterX    this)
        :viewport/top-gutter-height (FitViewport/.getTopGutterHeight this)
        :viewport/top-gutter-y      (FitViewport/.getTopGutterY      this)))))
