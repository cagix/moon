(ns cdq.game.dispose
  (:require [cdq.gdx.ui :as ui])
  (:import (com.badlogic.gdx.utils Disposable)))

(defn- dispose-world! [{:keys [world/tiled-map]}]
  (Disposable/.dispose tiled-map))

(defn- dispose-audio! [sounds]
  (run! Disposable/.dispose (vals sounds)))

(defn- dispose-cursors! [cursors]
  (run! Disposable/.dispose (vals cursors)))

(defn do!
  [{:keys [ctx/audio
           ctx/cursors
           ctx/batch
           ctx/shape-drawer-texture
           ctx/default-font
           ctx/textures
           ctx/world]}]
  (dispose-audio! audio)
  (dispose-cursors! cursors)
  (run! Disposable/.dispose (vals textures))
  (Disposable/.dispose batch)
  (Disposable/.dispose shape-drawer-texture)
  (when default-font
    (Disposable/.dispose default-font))
  (dispose-world! world)
  (ui/dispose!))
