(ns cdq.dispose
  (:import (com.badlogic.gdx.utils Disposable)))

(defn do! [ctx]
  #_(ctx-schema/validate ctx)
  (Disposable/.dispose ctx)
  ; TODO dispose world tiled-map/level resources?
  #_(dispose! @state))
