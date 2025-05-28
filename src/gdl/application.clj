(ns gdl.application
  (:require [qrecord.core :as q]))

(q/defrecord Context [ctx/assets
                      ctx/graphics
                      ctx/stage])
