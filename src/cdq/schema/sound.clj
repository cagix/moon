(ns cdq.schema.sound
  (:require [cdq.schema :as schema]))

(defmethod schema/malli-form :s/sound [_ _schemas]
  :string)
