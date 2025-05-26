(ns cdq.dispose
  (:import (com.badlogic.gdx.utils Disposable)))

(defn do! [{:keys [ctx/assets
                   ctx/graphics]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose graphics)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )
