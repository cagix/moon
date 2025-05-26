(ns cdq.dispose
  (:import (com.badlogic.gdx.utils Disposable)))

(defn do! [{:keys [ctx/assets
                   ctx/batch
                   ctx/shape-drawer-texture
                   ctx/cursors
                   ctx/default-font]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose batch)
  (Disposable/.dispose shape-drawer-texture)
  (run! Disposable/.dispose (vals cursors))
  (Disposable/.dispose default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
