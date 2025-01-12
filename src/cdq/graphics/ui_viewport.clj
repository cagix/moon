(ns cdq.graphics.ui-viewport
  (:require [clojure.gdx.utils.viewport.fit-viewport :as fit-viewport])
  (:import (com.badlogic.gdx.graphics OrthographicCamera)))

(defn create [_context config]
  {:pre [(:width  config)
         (:height config)]}
  (fit-viewport/create (:width  config)
                       (:height config)
                       (OrthographicCamera.)))
