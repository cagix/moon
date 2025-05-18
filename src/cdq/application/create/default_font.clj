(ns cdq.application.create.default-font
  (:require [cdq.ctx :as ctx]
            [cdq.utils :refer [bind-root]]
            [gdl.graphics :as graphics]))

(defn do! []
  (bind-root #'ctx/default-font (graphics/truetype-font (:default-font ctx/config))))
