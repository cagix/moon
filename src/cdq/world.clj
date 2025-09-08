(ns cdq.world
  (:require [qrecord.core :as q]))

(q/defrecord Entity [entity/body])
