(ns cdq.dispose
  (:import (com.badlogic.gdx.utils Disposable)))

(defn do! [{:keys [ctx/assets
                   ctx/batch
                   ctx/cursors
                   ctx/default-font
                   ctx/shape-drawer-texture]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose batch)
  (run! Disposable/.dispose (vals cursors))
  (Disposable/.dispose default-font)
  (Disposable/.dispose shape-drawer-texture)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
