(ns moon.schema.sound
  (:require [moon.schema :as schema]))

(defmethod schema/form :s/sound [_] :string)
