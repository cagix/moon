(ns clojure.gdx.utils.architecture
  (:import (com.badlogic.gdx.utils Architecture)))

(def mapping {Architecture/ARM       :architecture/arm
              Architecture/LOONGARCH :architecture/loongarch
              Architecture/RISCV     :architecture/riscv
              Architecture/x86       :architecture/x86})
