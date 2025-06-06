(ns gdl.c
  (:require [clojure.gdx.graphics.g2d.texture-region :as texture-region]
            [gdl.graphics :as graphics]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.ui.stage :as stage]
            [qrecord.core :as q]))

(defn world-mouse-position [{:keys [ctx/input
                                    ctx/graphics]}]
  (viewport/unproject (:world-viewport graphics) (input/mouse-position input)))

(defn ui-mouse-position [{:keys [ctx/input
                                 ctx/graphics]}]
  (viewport/unproject (:ui-viewport graphics) (input/mouse-position input)))

(defn mouseover-actor [{:keys [ctx/stage] :as ctx}]
  (stage/hit stage (ui-mouse-position ctx)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(q/defrecord Sprite [sprite/texture-region
                     sprite/pixel-dimensions
                     sprite/world-unit-dimensions
                     sprite/color]) ; optional

(defn- create-sprite
  [texture-region
   world-unit-scale]
  (let [scale 1 ; "scale can be a number for multiplying the texture-region-dimensions or [w h]."
        _ (assert (or (number? scale)
                      (and (vector? scale)
                           (number? (scale 0))
                           (number? (scale 1)))))
        pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-region/dimensions texture-region) scale)
                           scale)]
    (map->Sprite {:texture-region texture-region
                  :pixel-dimensions pixel-dimensions
                  :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale)})))

(defn sprite [{:keys [ctx/graphics]}
              texture-path]
  (create-sprite (texture-region/create (graphics/texture graphics texture-path))
                 (:world-unit-scale graphics)))

(defn sub-sprite [{:keys [ctx/graphics]} sprite [x y w h]]
  (create-sprite (texture-region/create (:sprite/texture-region sprite) x y w h)
                 (:world-unit-scale graphics)))

(defn sprite-sheet [{:keys [ctx/graphics]}
                    texture-path
                    tilew
                    tileh]
  {:image (create-sprite (texture-region/create (graphics/texture graphics texture-path))
                         (:world-unit-scale graphics))
   :tilew tilew
   :tileh tileh})

(defn sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
  (sub-sprite this image
              [(* x tilew)
               (* y tileh)
               tilew
               tileh]))
