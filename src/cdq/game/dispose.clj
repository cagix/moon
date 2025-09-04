(ns cdq.game.dispose
  (:require [cdq.gdx.ui :as ui])
  (:import (com.badlogic.gdx.utils Disposable)))

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
           ctx/tiled-map
           ctx/textures]}]
  (dispose-audio! audio)
  (dispose-cursors! cursors)
  (run! Disposable/.dispose (vals textures))
  (Disposable/.dispose batch)
  (Disposable/.dispose shape-drawer-texture)
  (when default-font
    (Disposable/.dispose default-font))
  (Disposable/.dispose tiled-map)
  (ui/dispose!))
