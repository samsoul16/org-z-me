(ns org-z-me.db
  (:require [clojure.spec.alpha :as s]))

;; spec of app-db
(s/def ::greeting string?)
(s/def ::app-db
  (s/keys :req-un [::greeting]))

;; initial state of app-db
(def app-db {:greeting "Hello Clojurescript in Expo!"
             :tasks [{:parent 0 :id 1 :name "Task1"}
                     {:parent 0 :id 2 :name "Task2"}
                     {:parent 1 :id 3 :name "Task3"}
                     {:parent 1 :id 4 :name "Task4"}
                     {:parent 2 :id 5 :name "Task5"}
                     {:parent 2 :id 6 :name "Task6"}
                     {:parent 3 :id 7 :name "Task7"}
                     {:parent 3 :id 8 :name "Task8"}
                     {:parent 4 :id 9 :name "Task9"}
                     {:parent 5 :id 10 :name "Task10"}
                     {:parent 6 :id 11 :name "Task11"}
                     {:parent 7 :id 12 :name "Task12"}]})
