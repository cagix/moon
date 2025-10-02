(ns cdq.graphics.dispose
  (:require [gdl.disposable :as disposable]))

(defn do! [{:keys [graphics/batch
                   graphics/cursors
                   graphics/default-font
                   graphics/shape-drawer-texture
                   graphics/textures]}]
  (disposable/dispose! batch)
  (run! disposable/dispose! (vals cursors))
  (disposable/dispose! default-font)
  (disposable/dispose! shape-drawer-texture)
  (run! disposable/dispose! (vals textures)))
