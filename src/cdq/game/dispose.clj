(ns cdq.game.dispose
  (:require [cdq.gdx.ui :as ui])
  (:import (com.badlogic.gdx.utils Disposable)))

(defn- dispose-world! [{:keys [world/tiled-map]}]
  (Disposable/.dispose tiled-map))

(defn- dispose-audio! [sounds]
  (run! Disposable/.dispose (vals sounds)))

(defn- dispose-cursors! [cursors]
  (run! Disposable/.dispose (vals cursors)))

(defn- dispose-graphics!
  [{:keys [g/batch
           g/shape-drawer-texture
           g/textures
           g/default-font]}]
  (Disposable/.dispose batch)
  (Disposable/.dispose shape-drawer-texture)
  (run! Disposable/.dispose (vals textures))
  (when default-font
    (Disposable/.dispose default-font)))

(defn do! [{:keys [ctx/audio
                   ctx/cursors
                   ctx/graphics
                   ctx/world]}]
  (dispose-audio! audio)
  (dispose-cursors! cursors)
  (dispose-graphics! graphics)
  (dispose-world! world)
  (ui/dispose!))
