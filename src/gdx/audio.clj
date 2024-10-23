(ns gdx.audio
  (:require [moon.component :refer [defc]]
            [moon.schema :as schema]
            [moon.tx :as tx])
  (:require [gdx.assets :as assets])
  (:import (com.badlogic.gdx.audio Sound)))

(defn play-sound! [path]
  (Sound/.play (assets/get path)))

(defmethod schema/form :s/sound [_] :string)

(defc :tx/sound
  {:schema :s/sound}
  (tx/handle [[_ file]]
    (play-sound! file)
    nil))
