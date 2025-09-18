(ns cdq.gdx.graphics
  (:require [com.badlogic.gdx.graphics.texture :as texture]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.utils :as utils]
            [space.earlygrey.shape-drawer :as sd]))

(defn draw-arc!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer
           center-x
           center-y
           radius
           (utils/degree->radians start-angle)
           (utils/degree->radians degree)))

(defn draw-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defn draw-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-circle!
  [{:keys [ctx/shape-drawer]}
   [x y] radius color]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defn draw-filled-ellipse!
  [{:keys [ctx/shape-drawer]}
   [x y] radius-x radius-y color]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defn draw-filled-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defn draw-line!
  [{:keys [ctx/shape-drawer]}
   [sx sy] [ex ey] color]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defn draw-rectangle!
  [{:keys [ctx/shape-drawer]}
   x y w h color]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defn draw-sector!
  [{:keys [ctx/shape-drawer]}
   [center-x center-y] radius start-angle degree color]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer
              center-x
              center-y
              radius
              (utils/degree->radians start-angle)
              (utils/degree->radians degree)))

(defn update-viewports!
  [{:keys [ctx/ui-viewport
           ctx/world-viewport]}
   width height]
  (viewport/update! ui-viewport    width height :center? true)
  (viewport/update! world-viewport width height :center? false))

(defn texture-region
  [{:keys [ctx/textures]}
   {:keys [image/file image/bounds]}]
  (assert file)
  (assert (contains? textures file))
  (let [texture (get textures file)]
    (if bounds
      (texture/region texture bounds)
      (texture/region texture))))
