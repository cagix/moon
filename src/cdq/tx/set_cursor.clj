(ns cdq.tx.set-cursor
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [clojure.graphics :as graphics]
            [clojure.utils :as utils]))

(defmethod do! :tx/set-cursor [[_ cursor-key]
                               {:keys [ctx/graphics
                                       ctx/cursors]}]
  (graphics/set-cursor! graphics (utils/safe-get cursors cursor-key)))
