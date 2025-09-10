(ns cdq.gdx-app.dispose
  (:require [clojure.gdx.utils.disposable :as disposable]
            [clojure.vis-ui :as vis-ui]))

(defn- dispose-audio! [sounds]
  (run! disposable/dispose! (vals sounds)))

(defn- dispose-cursors! [cursors]
  (run! disposable/dispose! (vals cursors)))

(defn do!
  [{:keys [ctx/audio
           ctx/cursors
           ctx/batch
           ctx/shape-drawer-texture
           ctx/default-font
           ctx/textures
           ctx/world
           ]}]
  (dispose-audio! audio)
  (dispose-cursors! cursors)
  (run! disposable/dispose! (vals textures))
  (disposable/dispose! batch)
  (disposable/dispose! shape-drawer-texture)
  (when default-font
    (disposable/dispose! default-font))
  (disposable/dispose! (:world/tiled-map world))
  (vis-ui/dispose!))
