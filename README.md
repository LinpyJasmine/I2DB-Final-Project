# Final Project

Can you extend a relational database system to support storing and querying over vectors?

## Steps

1. Fork this project
2. Understand the source code
3. Write your improvements
4. Run experiments
5. Write a report (Details can be seen below)
6. Push to GitLab and open a merge request


## How to Run
1. Start the server
2. Load the provided SIFT1M dataset
3. Stop the server to flush all the changes
4. Restart the server
5. Run the provided SIFT benchmark
6. Check the benchmark result

Note: Your improvement will be evaluated on the provided properties (1M items with 128-dimensional vector embedding each), same machine, same time limit(30 minutes for Loadtestbed+Benchmark excluding Recall Calculation).

## Note
The new workload includes `insert`. Please be sure to reload the testbed each time you run a benchmark.

## Hints

- We will only use `HeuristicQueryPlanner` for our vector search operations.
- Our naive implementation sorts all the vector and return the top-k closest records to the client.
- You can easily beat our performance by implementing any indexing algorithms for the vector search. Note that you still have to consider correctness because we will measure recall.
- Make sure `TablePlanner` calls your index, if you choose to implement one.
- You can look into `org.vanilladb.core.sql.distfn.EuclideanFn` to implement SIMD. Note: Our benchmark will only use `EuclideanFn`. You may choose not to implement SIMD for CosineFn.
- Make sure you run jdk17 with `jdk.incubator.vector` package (default jdk17 in VScode is not contain this package) to enable SIMD in Java.

## Experiments

Based on the workload we provide, show the followings:
- Throughput
- Recall

Show the comparison between the performance of the unmodified source code and the performance of your modification.

You can then think about the parameter settings that really show your improvements.

## Report

- Briefly explain what you do
    - How you implement your indexes
    - How you implement SIMD
    - Other improvements you made to speed up the search

- Experiments
    - Your experiment environment (a list of your hardware components, your operating system)
        - e.g. Intel Core i5-3470 CPU @ 3.2GHz, 16 GB RAM, 128 GB SSD, CentOS 7
    - Based on the workload we provide:
        - Show your improvement using graphs
    - Your benchmark parameters
    - Analysis on the results of the experiments

Note: There is no strict limitation to the length of your report. Generally, a 2-3 page report with some figures and tables is fine.
**Remember to include all your group members' student IDs**

## Submission

The procedure is as follows:
1. Fork the final project
2. Clone the repository you forked
3. Finish your work and write the report
4. Commit your work, push your work to GitLab.
    - Name your report `[Team Number]_final_project_report.pdf`
        - e.g. team1_final_project_report.pdf
5. Open a merge request to the original repository.
    - Source branch: Your working branch
    - Target branch: The branch with your team number (e.g. `team-1`)
    - Title: `Team-X Submission` (e.g. `Team-1 Submission`)

Note: Only one submission for each team.

## No Plagiarism

If we find you copying someone's code, you get 0 point for this assignment.

## No all data in memory

Database is not allowed to put all your data into memory.

## No modify our benchmark

modify our benchmark is not allowed.

## Deadline

Submit your work before **2025/6/1 (Sun) 23:59:59**.
