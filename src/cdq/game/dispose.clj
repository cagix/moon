(ns cdq.game.dispose
  (:require [cdq.gdx.ui :as ui])
  (:import (com.badlogic.gdx.utils Disposable)))

(defn- dispose-world! [{:keys [world/tiled-map]}]
  (Disposable/.dispose tiled-map))

(defn- dispose-audio! [sounds]
  (run! Disposable/.dispose (vals sounds)))

(defn- dispose-graphics!
  [{:keys [batch
           shape-drawer-texture
           textures
           cursors
           default-font]}]
  (Disposable/.dispose batch)
  (Disposable/.dispose shape-drawer-texture)
  (run! Disposable/.dispose (vals textures))
  (run! Disposable/.dispose (vals cursors))
  (when default-font
    (Disposable/.dispose default-font)))

(defn do! [{:keys [ctx/audio
                   ctx/graphics
                   ctx/world]}]
  (dispose-audio! audio)
  (dispose-graphics! graphics)
  (dispose-world! world)
  (ui/dispose!))
