(ns forge.graphics
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.viewport :as vp]
            [clojure.string :as str]
            [forge.app.asset-manager :refer [asset-manager]]
            [forge.app.sprite-batch :refer [batch]]
            [forge.app.default-font :refer [default-font]]
            [forge.app.shape-drawer :refer [with-line-width]]
            [forge.app.world-viewport :refer [world-unit-scale
                                              world-viewport]])
  (:import (com.badlogic.gdx.graphics.g2d BitmapFont)
           (com.badlogic.gdx.utils Align)))

(defn- draw-texture-region [batch texture-region [x y] [w h] rotation color]
  (if color (.setColor batch color))
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; rotation origin
         (/ (float h) 2)
         w ; width height
         h
         1 ; scaling factor
         1
         rotation)
  (if color (.setColor batch color/white)))

(defn- draw-on-viewport [batch viewport draw-fn]
  (.setColor batch color/white) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (.combined (vp/camera viewport)))
  (.begin batch)
  (draw-fn)
  (.end batch))

(def ^:dynamic ^:private *unit-scale* 1)

(defn- text-height [^BitmapFont font text]
  (-> text
      (str/split #"\n")
      count
      (* (.getLineHeight font))))

(defn- gdx-align [k]
  (case k
    :center Align/center
    :left   Align/left
    :right  Align/right))

(defn draw-text
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [font x y text h-align up? scale]}]
  (let [^BitmapFont font (or font default-font)
        data (.getData font)
        old-scale (float (.scaleX data))]
    (.setScale data (* old-scale
                       (float *unit-scale*)
                       (float (or scale 1))))
    (.draw font
           batch
           (str text)
           (float x)
           (+ (float y)
              (float (if up?
                       (text-height font text)
                       0)))
           (float 0) ; target-width
           (gdx-align (or h-align
                          :center))
           false) ; wrap false, no need target-width
    (.setScale data old-scale)))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn draw-image
  [{:keys [texture-region color] :as image} position]
  (draw-texture-region batch
                       texture-region
                       position
                       (unit-dimensions image *unit-scale*)
                       0 ; rotation
                       color))

(defn draw-rotated-centered
  [{:keys [texture-region color] :as image} rotation [x y]]
  (let [[w h] (unit-dimensions image *unit-scale*)]
    (draw-texture-region batch
                         texture-region
                         [(- (float x) (/ (float w) 2))
                          (- (float y) (/ (float h) 2))]
                         [w h]
                         rotation
                         color)))

(defn draw-centered [image position]
  (draw-rotated-centered image 0 position))

(defn- draw-with [viewport unit-scale draw-fn]
  (draw-on-viewport batch
                    viewport
                    #(with-line-width unit-scale
                       (fn []
                         (binding [*unit-scale* unit-scale]
                           (draw-fn))))))

(defn draw-on-world-view [render-fn]
  (draw-with world-viewport
             world-unit-scale
             render-fn))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- texture-dimensions [texture-region]
  [(g/region-width  texture-region)
   (g/region-height texture-region)])

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defn- sprite* [texture-region]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1) ; = scale 1
      map->Sprite))

(defn ->image [path]
  (sprite* (g/texture-region (asset-manager path))))

(defn sub-image [image bounds]
  (sprite* (apply g/->texture-region (:texture-region image) bounds)))

(defn sprite-sheet [path tilew tileh]
  {:image (->image path)
   :tilew tilew
   :tileh tileh})

(defn ->sprite [{:keys [image tilew tileh]} [x y]]
  (sub-image image
             [(* x tilew) (* y tileh) tilew tileh]))

(defn edn->image [{:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (->sprite (sprite-sheet file tilew tileh)
                [(int (/ sprite-x tilew))
                 (int (/ sprite-y tileh))]))
    (->image file)))
