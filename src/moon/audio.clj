(ns moon.audio
  (:require [moon.assets :as assets]
            [moon.component :refer [defc]]
            [moon.schema :as schema]
            [moon.tx :as tx])
  (:import (com.badlogic.gdx.audio Sound)))

(defn play-sound! [path]
  (Sound/.play (get assets/manager path)))

(defmethod schema/form :s/sound [_] :string)

(defc :tx/sound
  {:schema :s/sound}
  (tx/handle [[_ file]]
    (play-sound! file)
    nil))
