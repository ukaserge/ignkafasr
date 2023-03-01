
def on_acked_print(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Message produced: %s" % (str(msg)))

def on_commit_completed_print(err, partitions):
    if err:
        print(str(err))
    else:
        print("Committed partition offsets: " + str(partitions))

def print_assignment(topic_consumer, partitions):
    print('Assignment:', partitions)
