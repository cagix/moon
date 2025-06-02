(ns clojure.create.graphics
  (:require [clojure.graphics :as graphics])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [ctx]
  (assoc ctx :ctx/graphics (let [this Gdx/graphics]
                             (reify graphics/Graphics
                               (delta-time [_]
                                 (.getDeltaTime this))

                               (frames-per-second [_]
                                 (.getFramesPerSecond this))

                               (new-cursor [_ pixmap hotspot-x hotspot-y]
                                 (.newCursor this pixmap hotspot-x hotspot-y))

                               (set-cursor! [_ cursor]
                                 (.setCursor this cursor))))))
